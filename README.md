#scala-aws-lambda

A template project for Scala on AWS Lambda  
This project uses [sbt-aws-lambda](https://github.com/gilt/sbt-aws-lambda) plugin, and it uses [DefaultAWSCredentialsProviderChain](http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/DefaultAWSCredentialsProviderChain.html) to retreive the AWS credentials from your environemnt.  
It will look for credentials in this order:  
- Environment Variables - AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY
- Java System Properties - aws.accessKeyId and aws.secretKey
- Credential profiles file at the default location (~/.aws/credentials)
 ```
 [default]
aws_access_key_id = your_aws_access_key_id
aws_secret_access_key = your_aws_secret_access_key
 ```
 
- Credentials delivered through the Amazon EC2 container service
- Instance profile credentials delivered through the Amazon EC2 metadata service

Deployment
-------------
`sbt createLambda` creates a new AWS Lambda function from the current project.

`sbt updateLambda` updates an existing AWS Lambda function with the current project.
