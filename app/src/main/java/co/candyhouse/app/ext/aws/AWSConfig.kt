package co.candyhouse.app.ext.aws

import co.candyhouse.app.BuildConfig

class AWSConfig {
    companion object {
        const val jpDevTeam = """
                {
                  "Version": "1.0",
                  "CredentialsProvider": {
                    "CognitoIdentity": {
                      "Default": {
                        "PoolId": "${BuildConfig.AWS_IDENTITY_POOL_ID}",
                        "Region": "ap-northeast-1"
                      }
                    }
                  },

                  "CognitoUserPool": {
                    "Default": {
                      "AppClientId": "${BuildConfig.AWS_APP_CLIENT_ID}",
                      "PoolId": "${BuildConfig.AWS_USER_POOL_ID}",
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