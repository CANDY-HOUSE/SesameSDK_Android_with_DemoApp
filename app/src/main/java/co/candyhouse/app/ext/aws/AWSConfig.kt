package co.candyhouse.app.ext.aws

class AWSConfig {
    companion object {
        const val jpDevTeam = """
                {
                  "Version": "1.0",
                  "CredentialsProvider": {
                    "CognitoIdentity": {
                      "Default": {
                        "PoolId": "YOUR_PoolId",
                        "Region": "ap-northeast-1"
                      }
                    }
                  },

                  "CognitoUserPool": {
                    "Default": {
                      "AppClientId": "YOUR_AppClientId",
                      "PoolId": "YOUR_PoolId",
                      "Region": "ap-northeast-1"
                    }
                  },
                    "Auth": {
                     "Default": {
                          "authenticationFlowType": "CUSTOM_AUTH"
                      }
                    }
                }
                """
    }
}