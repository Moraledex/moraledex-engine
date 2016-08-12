name := "moraledex-engine"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-lambda-java-core" % "1.1.0" exclude("commons-logging", "commons-logging"),
  "com.amazonaws" % "aws-lambda-java-events" % "1.1.0" exclude("commons-logging", "commons-logging"),
  "com.github.seratch" %% "awscala" % "0.5.6" exclude("commons-logging", "commons-logging"),
  "com.typesafe.play" %% "play-ws" % "2.4.8"
)

retrieveManaged := true

enablePlugins(AwsLambdaPlugin)

lambdaHandlers := Seq(
  "updateSentimentDb"   -> "hck.moraledex.lambda.FeedbackAnalyzer::updateSentimentDb",
  "getSentiments"       -> "hck.moraledex.lambda.FeedbackAnalyzer::getSentiments"
)

s3Bucket := Some("lambda-scala")

awsLambdaMemory := Some(192)

awsLambdaTimeout := Some(300)

//you have to change this to be your own Role ARN

roleArn := Some("arn:aws:iam::617692333704:role/lambda_basic_execution")
