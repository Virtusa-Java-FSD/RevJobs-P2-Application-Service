pipeline {
    agent any

    triggers {
        githubPush()
        pollSCM('H/5 * * * *')
    }

    environment {
        REMOTE_HOST = "${env.EC2_SERVICES_IP}"
        REMOTE_USER = "ec2-user"
        REMOTE_DIR = "/home/ec2-user/revjobs/revjob_p1_microservices/application-service/target"
        SSH_CREDENTIALS_ID = "ec2-ssh-key"
    }

    tools {
        maven 'Maven 3'
        jdk 'JDK 17'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                bat 'mvn clean package -DskipTests'
            }
        }

        stage('Deploy to EC2') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: 'ec2-ssh-key', keyFileVariable: 'SSH_KEY')]) {
                    powershell '''
                        # 1. Fix Key Permissions
                        $keyPath = $env:SSH_KEY
                        $acl = Get-Acl $keyPath
                        $acl.SetAccessRuleProtection($true, $false)
                        $currentUser = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
                        $rule = New-Object System.Security.AccessControl.FileSystemAccessRule($currentUser, "FullControl", "Allow")
                        $acl.SetAccessRule($rule)
                        Set-Acl $keyPath $acl

                        # 2. Deployment Steps
                        $remote = "$env:REMOTE_USER@$env:REMOTE_HOST"
                        
                        # Stop service
                        ssh -i $keyPath -o StrictHostKeyChecking=no $remote "sudo systemctl stop application-service"
                        
                        # Create dir
                        ssh -i $keyPath -o StrictHostKeyChecking=no $remote "mkdir -p $env:REMOTE_DIR"
                        
                        # Copy new jar
                        $jarFile = Get-Item "target/*.jar"
                        scp -i $keyPath -o StrictHostKeyChecking=no $jarFile $remote":"$env:REMOTE_DIR/application-service-1.0.0.jar
                        
                        # Start service
                        ssh -i $keyPath -o StrictHostKeyChecking=no $remote "sudo systemctl start application-service"
                    '''
                }
            }
        }
    }
}
