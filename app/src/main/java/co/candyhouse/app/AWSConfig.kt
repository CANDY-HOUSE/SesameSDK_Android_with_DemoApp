package co.candyhouse.app


class AWSConfig {
    companion object {
        val testConfig = """
                {
                  "Version": "1.0",
                  "CredentialsProvider": {
                    "CognitoIdentity": {
                      "Default": {
                        "PoolId": "us-east-2:84153b0c-5b1f-462d-8adc-d9d2581f33c2",
                        "Region": "us-east-2"
                      }
                    }
                  },
                  "IdentityManager": {
                    "Default": {}
                  },
                  "CognitoUserPool": {
                    "Default": {
                      "AppClientSecret": "1k1ni8bnjifjpsl2pg9n2061ln7ja1hdan2ptkdu7b5ups44ud8d",
                      "AppClientId": "21v9tlqp4qtjbau7k1epb15n8f",
                      "PoolId": "us-east-1_69JF5fktv",
                      "Region": "us-east-1"
                    }
                  }
                }
                """
        val prodConfig = """
                {
                  "Version": "1.0",
                  "CredentialsProvider": {
                    "CognitoIdentity": {
                      "Default": {
                        "PoolId": "us-east-2:84153b0c-5b1f-462d-8adc-d9d2581f33c2",
                        "Region": "us-east-2"
                      }
                    }
                  },
                  "IdentityManager": {
                    "Default": {}
                  },
                  "CognitoUserPool": {
                    "Default": {
                      "AppClientSecret": "8764l2phlhb6pgb8q54fia39hrq6l25munhmu9v0773prg1fk6m",
                      "AppClientId": "7606ec2uvh35kr0e2oe0oap4n2",
                      "PoolId": "us-east-2_BQTDbVv3q",
                      "Region": "us-east-2"
                    }
                  }
                }
                """

        val jpConfig = """
                {
                  "Version": "1.0",
                  "CredentialsProvider": {
                    "CognitoIdentity": {
                      "Default": {
                        "PoolId": "ap-northeast-1:743e979a-3c32-4162-adbe-3432fd3e25e9",
                        "Region": "ap-northeast-1"
                      }
                    }
                  },
                  "IdentityManager": {
                    "Default": {}
                  },
                  "CognitoUserPool": {
                    "Default": {
                      "AppClientSecret": "o50qhtac66cgskf170kch9c3na63qu8j7i998lftkatntta0obd",
                      "AppClientId": "3sul7hj8fpe2vviggmmernbtve",
                      "PoolId": "ap-northeast-1_7De0EV29N",
                      "Region": "ap-northeast-1"
                    }
                  }
                }
                """
    }
}