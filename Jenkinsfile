pipeline {
    agent any

    environment {
        REMOTE_HOST = "${env.EC2_SERVICES_IP}"
        REMOTE_USER = "ec2-user"
        REMOTE_DIR = "/home/ec2-user/microservices/application-service"
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
                sshagent(['ec2-ssh-key']) {
                     try {
                         bat "ssh -o StrictHostKeyChecking=no ${REMOTE_USER}@${REMOTE_HOST} 'pkill -f application-service || true'"
                     } catch (Exception e) { echo 'Process not found' }

                     bat "ssh -o StrictHostKeyChecking=no ${REMOTE_USER}@${REMOTE_HOST} 'mkdir -p ${REMOTE_DIR}'"
                     bat "scp -o StrictHostKeyChecking=no target/*.jar ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_DIR}/application-service.jar"
                     bat "ssh -o StrictHostKeyChecking=no ${REMOTE_USER}@${REMOTE_HOST} 'nohup java -jar ${REMOTE_DIR}/application-service.jar > ${REMOTE_DIR}/log.txt 2>&1 &'"
                }
            }
        }
    }
}
