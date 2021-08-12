pipeline {

   agent {
      label 'aep-builder'
   }

   triggers {
      GenericTrigger(
         genericVariables: [
            [
               key: 'objectKind',
               value: '$.object_kind',
               expressionType: 'JSONPath'
            ],
            [
               key: 'gitHTTPURL',
               value: '$.repository.git_http_url',
               expressionType: 'JSONPath'
            ],
            [
               key: 'gitSSHURL',
               value: '$.repository.git_ssh_url',
               expressionType: 'JSONPath'
            ],
            [
               key: 'ref',
               value: '$.ref',
               expressionType: 'JSONPath'
            ],
            [
               key: 'after',
               value: '$.after',
               expressionType: 'JSONPath'
            ],
            [
               key: 'project_name',
               value: '$.repository.name',
               expressionType: 'JSONPath'
            ],
            [
               key: 'user_email',
               value: '$.commits[0].author.email',
               expressionType: 'JSONPath'
            ],
            [
               key: 'main_user_email',
               value: '$.user_email',
               expressionType: 'JSONPath'
            ]
         ],
         genericRequestVariables: [
            [
               key: 'image_name'
            ]
         ],

         causeString: 'Triggered on $ref',

         token: env.JOB_NAME,

         printContributedVariables: true,
         printPostContent: true,

         silentResponse: false,
      )
   }

   environment {
      // Load in environment
      after = "${after}"
      mainUserEmailValue = "${main_user_email}"
      userEmailValue = "${user_email}"
      projectNameValue = "${project_name}"
      imageNameValue = "${image_name}"
      // Image Full Path
      dockerRepositoryValue = sh (script: """
         if [[ "${objectKind}" == "tag_push" ]]; then
            tag_name=`echo ${ref} | awk -F'refs/tags/' '{print \$2}' | cut -c 1-4`
            if [[ "\${tag_name}" == "gie." ]]; then
               echo "ccr.ccs.tencentyun.com/gizwits_gie"
            else
               echo "ccr.ccs.tencentyun.com/gizwits_gems"
            fi
         else
            echo "ccr.ccs.tencentyun.com/gizwits_gems"
         fi
      """, returnStdout: true).trim()
      // Branche Value such as `tag/x.x.x` or `*/release/1.2.3`
      brancheValue = sh (script: """
         if [[ "${objectKind}" == "tag_push" ]]; then
            echo ${ref} | awk -F'refs/' '{print \$2}'
         elif [[ "${objectKind}" == "push" && ${ref} != "refs/heads/master" ]]; then
            echo ${ref} | sed 's/refs\\/heads/*/'
         else
            echo "null"
         fi
      """, returnStdout: true).trim()
      // Tag Value such as `1.2.3` or `release-1.2.3-abcdef`
      tagValue = sh (script: """
         if [[ "${objectKind}" == "tag_push" ]]; then
            echo ${ref} | awk -F'/' '{print \$3}'
         elif [[ "${objectKind}" == "push" && ${ref} != "refs/heads/master" ]]; then
            prefix=`echo ${ref} | awk -F'refs/heads/' '{print \$2}' | sed 's/\\//-/'`
            suffix=`echo ${after} | cut -c 1-6`
            echo \${prefix}-\${suffix}
         else
            echo "null"
         fi
      """, returnStdout: true).trim()
   }

   stages {
      stage('Info') {
         steps {
            echo "dockerRepositoryValue ${dockerRepositoryValue}"
            echo "imageNameValue ${imageNameValue}"
            echo "brancheValue ${brancheValue}"
            echo "tagValue ${tagValue}"
            echo "userEmail ${userEmailValue}"
         }
      }

      stage('Pull') {
         when {
            allOf {
               expression {
                  return brancheValue != "null"
               }
               expression {
                  return imageNameValue != "null"
               }
               expression {
                  return tagValue != "null"
               }
               expression {
                  return after != "0000000000000000000000000000000000000000"
               }
            }
         }
         steps {
            checkout([
                $class: 'GitSCM',
                branches: [[name: "${brancheValue}"]],
                doGenerateSubmoduleConfigurations: true,
                extensions: [[
                    $class: 'CleanBeforeCheckout'
                ],[
                    $class: 'CleanCheckout'
                ],[
                    $class: 'SubmoduleOption', recursiveSubmodules: true, trackingSubmodules: true, parentCredentials: true, timeout: 120
                ]],
                submoduleCfg: [],
                userRemoteConfigs: [[
                    credentialsId: 'opsci',
                    url: "${gitHTTPURL}"
                ]]
            ])
         }
      }

      stage('Test') {
         when {
            allOf {
               expression {
                  return brancheValue != "null"
               }
               expression {
                  return imageNameValue != "null"
               }
               expression {
                  return tagValue != "null"
               }
               expression {
                  return after != "0000000000000000000000000000000000000000"
               }
            }
         }
         steps {
            echo "Test"
         }
      }

      stage('Build And Push') {
         when {
            allOf {
               expression {
                  return brancheValue != "null"
               }
               expression {
                  return imageNameValue != "null"
               }
               expression {
                  return tagValue != "null"
               }
               expression {
                  return after != "0000000000000000000000000000000000000000"
               }
            }
         }
         steps {
            sh """
            if [ -f "./build/Dockerfile" ]; then
                docker buildx build . -t ${dockerRepositoryValue}/${imageNameValue}:${tagValue} -f ./build/Dockerfile --platform linux/amd64 --build-arg VERSION=${tagValue} --push
            else
                docker buildx build . -t ${dockerRepositoryValue}/${imageNameValue}:${tagValue} -f ./Dockerfile --platform linux/amd64 --build-arg VERSION=${tagValue} --push
            fi
            """
         }
      }
   }


   post {
      always {
         echo "Send notifications for result: ${currentBuild.result}, and display name: ${currentBuild.fullDisplayName}"
      }

      success {
         echo "This project is success"
         sh "curl -LO https://jzhuang-1254961755.cos.ap-guangzhou.myqcloud.com/email.html"
         script {
            def emailNotify = readFile file: './email.html', encoding: 'UTF-8'
            if ("${env.brancheValue}" != "null" && "${env.after}" != "0000000000000000000000000000000000000000") {
               if ("${env.userEmailValue}" != "null") {
                  emailext to: "${env.userEmailValue}", subject: "${env.projectNameValue}"+' - Build $BUILD_STATUS - '+"DockerImage: ${dockerRepositoryValue}/${imageNameValue}:${tagValue}", body: "${emailNotify}"
               }
               if ("${env.mainUserEmailValue}" != "null") {
                  emailext to: "${env.mainUserEmailValue}", subject: "${env.projectNameValue}"+' - Build $BUILD_STATUS - '+"DockerImage: ${dockerRepositoryValue}/${imageNameValue}:${tagValue}", body: "${emailNotify}"
               }
            }
         }
      }

      failure {
         echo "This project is fail"
         sh "curl -LO https://jzhuang-1254961755.cos.ap-guangzhou.myqcloud.com/email.html"
         script {
            def emailNotify = readFile file: './email.html', encoding: 'UTF-8'
            if ("${env.brancheValue}" != "null" && "${env.after}" != "0000000000000000000000000000000000000000") {
               if ("${env.userEmailValue}" != "null") {
                  emailext to: "${env.userEmailValue}", subject: "${env.projectNameValue}"+' - Build $BUILD_STATUS - '+"DockerImage: ${dockerRepositoryValue}/${imageNameValue}:${tagValue}", body: "${emailNotify}"
               }
               if ("${env.mainUserEmailValue}" != "null") {
                  emailext to: "${env.mainUserEmailValue}", subject: "${env.projectNameValue}"+' - Build $BUILD_STATUS - '+"DockerImage: ${dockerRepositoryValue}/${imageNameValue}:${tagValue}", body: "${emailNotify}"
               }
            }
         }
      }
   }

}