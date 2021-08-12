
node	{
    stage("cheakout"){
        checkout([$class: 'GitSCM', branches: [[name: '**']], extensions: [], userRemoteConfigs: [[credentialsId: 'e41f358d-3d20-4c4e-99cb-8726e11819a2', url: 'git@github.com:yc13456/Go-project.git']]])
    }
	stage("build Images"){
		
		sh """
            if [ -f "Dockerfile" ]; then
                docker buildx build -f Dockerfile --platform 'linux/arm64'  -t '2267024990/hello' . '--push'
            else
                echo "no Dockerfile"
            fi
            """
	
    }
    stage("resultds"){
        echo "result"
	echo "result"
    }
}

