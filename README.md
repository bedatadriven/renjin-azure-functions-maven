
# Deploying a Predictive Model to Azure Functions with Renjin

Renjin is a JVM-based alternative engine for the R Language that aims to
simplify deployment of R language code across a wide range of
environments.

Because Renjin runs R code on the JVM, deploying an R language
predictive model to Azure Functions is straightforward.

This example project builds on the [Azure Function Java/Maven Quickstart](https://docs.microsoft.com/en-us/azure/azure-functions/functions-create-first-java-maven)
from the Azure documentation.

## Prerequisites

* Java Development Kit Version 1.8 or higher.
* [Apache Maven 3](https://maven.apache.org/download.cgi)
* You have already registered your account on Azure.
* You have already installed [Azure CLI](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli?view=azure-cli-latest) and signed in to the Azure.
* You have installed the [Azure Functions Core Tools](https://docs.microsoft.com/en-us/azure/azure-functions/functions-run-local#v2)

## Prepare your model and scoring function

Once you have trained your model, serialize it to an .RData file and
add this file to [src/main/resources](src/main/resources).

Add a script that contains a `score` function that will be invoked
by Azure functions.

```.R
score <- function(request) {
   # ... do something
}
```

The `request` object includes the request method, query parameters,
and headers:

```
> str(request)
List of 3
 $ method         : chr "GET"
 $ queryParameters:List of 3
  ..$ balance   : chr "100000"
  ..$ numTrans  : chr "400"
  ..$ creditLine: chr "1000000"
 $ headers        :List of 9
  ..$ connection               : chr "keep-alive"
  ..$ accept                   : chr "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3"
  ..$ accept-encoding          : chr "gzip, deflate, br"
  ..$ accept-language          : chr "en-US,en;q=0.9,fr;q=0.8,nl;q=0.7"
  ..$ cookie                   : chr "_ga=GA1.1.1619650480.1557387501; __distillery=d309ece_60fdd0f0-b57b-49dd-a1c4-372ddfde97a8-74b00ee05-6e766be8c12c-0bda"
  ..$ host                     : chr "localhost:7071"
  ..$ user-agent               : chr "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.131 Safari/537.36"
  ..$ upgrade-insecure-requests: chr "1"
  ..$ dnt                      : chr "1"
```

The `function` should return a character string with json. In our
example, the complete score.R is:

```.R

load('res:fraudModel.rData')

score <- function(request) {

    input <- data.frame(
                balance = as.double(request$query$balance),
                numTrans = as.double(request$query$numTrans),
                creditLine = as.double(request$query$creditLine))

     p <- predict(fraudModel, input)[1]

     rjson::toJSON(list(score = as.double(p)))
}
```

## Add dependencies

You can add dependencies on specific versions of CRAN and BioConductor
packages. Visit https://packages.renjin.org to find the most recent
build of the package version you are using. Add it to the dependencies
section of the pom.xml file:

```.xml
    <dependencies>
        <dependency>
            <groupId>org.renjin.cran</groupId>
            <artifactId>rpart</artifactId>
            <version>4.1-15-b1</version>
        </dependency>
    </dependencies>

```

## Test locally

You can test your script locally with Maven:

```
mvn clean package
mvn azure-functions:run
```

```.sh
curl "http://localhost:7071/api/score?balance=40&numTrans=400&creditLine=100"
{"score": 0.5684210526315789}
```

## Deploy to Azure

Once your function is working as expected, you can deploy to
Azure:

```.sh
az login
mvn azure-functions:deploy
```

## Authentication

Be careful! The authentication level for this example is set to
anonymous in [RenjinFunction](src/main/java/org/renjin/azure/RenjinFunction.java).

You may need to adapt this code to meet your needs in production.

## Learn more

Visit [renjin.org](https://www.renjin.org) to learn more about Renjin, or visit our documentation at
[docs.renjin.org](http://docs.renjin.org). Need help professionalizing your cloud or on-premise R deployments?
Contact us at support@renjin.org or on Twitter at [@bedatadriven](https://twitter.com/bedatadriven)

# License

The code in this repo is MIT licensed. The rpart package is licensed under GPL 2 | 3, and Renjin is GPL 2 licensed.
