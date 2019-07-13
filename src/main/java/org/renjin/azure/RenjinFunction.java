package org.renjin.azure;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import org.renjin.eval.EvalException;
import org.renjin.script.RenjinScriptEngine;
import org.renjin.script.RenjinScriptEngineFactory;
import org.renjin.sexp.*;

import javax.script.ScriptException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

public class RenjinFunction {

    private static final ThreadLocal<RenjinScriptEngine> ENGINE = new ThreadLocal<>();

    @FunctionName("score")
    public HttpResponseMessage run(@HttpTrigger(name = "req", methods = HttpMethod.GET, authLevel = AuthorizationLevel.ANONYMOUS)
                                           HttpRequestMessage<Optional<String>> request, final ExecutionContext context) throws ScriptException, NoSuchMethodException, IOException {

        RenjinScriptEngine engine = ENGINE.get();
        if(engine == null) {
            engine = new RenjinScriptEngineFactory().getScriptEngine();
            try(Reader in = new InputStreamReader(getClass().getResourceAsStream("/score.R"), StandardCharsets.UTF_8)) {
                engine.eval(in);
            }
            ENGINE.set(engine);
        }

        ListVector.NamedBuilder query = new ListVector.NamedBuilder();
        for (Map.Entry<String, String> parameter : request.getQueryParameters().entrySet()) {
            query.add(parameter.getKey(), parameter.getValue());
        }

        ListVector.NamedBuilder headers = new ListVector.NamedBuilder();
        for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
            headers.add(header.getKey(), header.getValue());
        }

        ListVector.NamedBuilder requestList = new ListVector.NamedBuilder();
        requestList.add("method", request.getHttpMethod().name());
        requestList.add("queryParameters", query.build());
        requestList.add("headers", headers.build());

        SEXP result;
        try {
            result = engine.invokeFunction("score", requestList.build());
        } catch (EvalException e) {
            e.printRStackTrace(System.err);
            throw e;
        }

        StringVector characterResult = (StringVector) engine.invokeFunction("as.character", result);

        StringBuilder stringResponse = new StringBuilder();
        for (String line : characterResult) {
            stringResponse.append(line).append('\n');
        }

        return request.createResponseBuilder(HttpStatus.OK)
            .body(stringResponse.toString())
            .header("Content-Type", "application/json")
            .build();

    }
}
