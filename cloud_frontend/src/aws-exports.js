// src/aws-exports.js
const awsConfig = {
	aws_project_region: 'us-east-1',
	aws_cognito_region: 'us-east-1',
	aws_user_pools_id: 'us-east-1_hF0SbfZz3', // Your User Pool ID
	aws_user_pools_web_client_id: '25girlchh3olm20j31mkp5fpa8', // Your App Client ID
	oauth: {
		domain: 'us-east-1hf0sbfzz3.auth.us-east-1.amazoncognito.com', // NO "https://" prefix!
		scope: ['email', 'openid', 'phone'],
		redirectSignIn: 'http://localhost:5173', // Must match Cognito settings EXACTLY
		redirectSignOut: 'http://localhost:5173', // Must match Cognito settings EXACTLY
		responseType: 'code'
	}
};

export default awsConfig;