
# Load them model. This is done once per thread, not for
# every request.

load('res:fraudModel.rData')


score <- function(request) {

     input <- data.frame(
                balance = as.double(request$query$balance),
                numTrans = as.double(request$query$numTrans),
                creditLine = as.double(request$query$creditLine))

     p <- predict(fraudModel, input)[1]

     rjson::toJSON(list(score = as.double(p)))
}