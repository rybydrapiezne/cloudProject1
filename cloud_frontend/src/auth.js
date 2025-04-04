// src/auth.js
import { Amplify } from 'aws-amplify';
import awsConfig from './aws-exports';
import { signOut, signInWithRedirect, getCurrentUser } from 'aws-amplify/auth';

// 1. Configure Amplify FIRST
Amplify.configure(awsConfig);

// 2. Redirect to Cognito Hosted UI
export const login = () => {
	console.log('Initiating Cognito redirect...');
	signInWithRedirect(); // This is the critical line
};

// 3. Handle logout
export const logout = () => signOut();

// 4. Check auth status
export const checkAuth = async () => {
	try {
		return await getCurrentUser();
	} catch {
		return null;
	}
};