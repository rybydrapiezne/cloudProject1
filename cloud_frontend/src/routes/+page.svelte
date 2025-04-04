<script lang="ts">
	import { onMount, afterUpdate } from 'svelte';
	import api from '$lib/api';
	import type { Message } from '$lib/types/Message';
	import { login, logout, checkAuth } from '../auth';
	import { goto } from '$app/navigation';
	import { Hub } from 'aws-amplify/utils';
	import { fetchAuthSession } from 'aws-amplify/auth';

	// Define the possible auth event types
	type AuthEvent = 'signIn' | 'signOut' | 'tokenRefresh' | 'signIn_failure' | 'signOut_failure';

	// State variables
	let username: string = 'User' + Math.floor(Math.random() * 1000);
	let newMessage: string = '';
	let messages: Message[] = [];
	let chatContainer: HTMLDivElement | undefined;
	let isAuthenticated: boolean = false;
	let authChecked: boolean = false;
	let errorMessage: string = '';

	// Utility to scroll chat to the bottom
	const scrollToBottom = () => {
		if (chatContainer) {
			chatContainer.scrollTop = chatContainer.scrollHeight;
		}
	};

	// Fetch all messages from the chat
	const fetchAllMessages = async () => {
		if (typeof window === 'undefined') return;
		try {
			const session = await fetchAuthSession();
			const token = session.tokens?.idToken?.toString();
			if (!token) throw new Error('No ID token available');
			const response = await api.get('/chat/all', {
				params: { username },
				headers: { Authorization: `Bearer ${token}` }
			});
			messages = response.data.messages || [];
			scrollToBottom();
		} catch (error) {
			// Enhanced error handling
			const errorMsg = error instanceof Error ? error.message : String(error || 'Unknown error');
			const errorDetails = error && typeof error === 'object' ? JSON.stringify(error) : 'No details available';
			console.error('Error fetching all messages:', errorMsg, 'Details:', errorDetails);
			errorMessage = 'Failed to load messages. Please try again.';
		}
	};

	// Fetch new messages since the last timestamp
	const fetchNewMessages = async () => {
		if (typeof window === 'undefined') return;
		try {
			const session = await fetchAuthSession();
			const token = session.tokens?.idToken?.toString();
			if (!token) throw new Error('No ID token available');
			const lastTimestamp = messages.length
				? messages[messages.length - 1].timestamp
				: new Date(0).toISOString();
			const response = await api.get('/chat', {
				params: { username, after: lastTimestamp },
				headers: { Authorization: `Bearer ${token}` }
			});
			if (response.data.messages?.length) {
				messages = [...messages, ...response.data.messages];
				scrollToBottom();
			}
		} catch (error) {
			const errorMsg = error instanceof Error ? error.message : String(error || 'Unknown error');
			const errorDetails = error && typeof error === 'object' ? JSON.stringify(error) : 'No details available';
			console.error('Error fetching new messages:', errorMsg, 'Details:', errorDetails);
			errorMessage = 'Failed to fetch new messages.';
		}
	};

	// Send a new message
	const sendMessage = async () => {
		if (typeof window === 'undefined' || !isAuthenticated || !newMessage.trim()) return;
		try {
			const session = await fetchAuthSession();
			const token = session.tokens?.idToken?.toString();
			if (!token) throw new Error('No ID token available');
			await api.post('/chat', { username, message: newMessage }, {
				headers: { Authorization: `Bearer ${token}` }
			});
			newMessage = '';
		} catch (error) {
			const errorMsg = error instanceof Error ? error.message : String(error || 'Unknown error');
			const errorDetails = error && typeof error === 'object' ? JSON.stringify(error) : 'No details available';
			console.error('Error sending message:', errorMsg, 'Details:', errorDetails);
			errorMessage = 'Failed to send message.';
		}
	};

	// Handle login action
	const handleLogin = async () => {
		try {
			await login();
		} catch (error) {
			const errorMsg = error instanceof Error ? error.message : String(error || 'Unknown error');
			const errorDetails = error && typeof error === 'object' ? JSON.stringify(error) : 'No details available';
			console.error('Login error:', errorMsg, 'Details:', errorDetails);
			errorMessage = 'Login failed.';
		}
	};

	// Handle logout action
	const handleLogout = async () => {
		try {
			await logout();
			isAuthenticated = false;
			authChecked = true;
			goto('/');
		} catch (error) {
			const errorMsg = error instanceof Error ? error.message : String(error || 'Unknown error');
			const errorDetails = error && typeof error === 'object' ? JSON.stringify(error) : 'No details available';
			console.error('Logout error:', errorMsg, 'Details:', errorDetails);
			errorMessage = 'Logout failed.';
		}
	};

	// On component mount: set up interval and initial auth check
	onMount(async () => {
		// Set up interval to fetch new messages periodically if authenticated
		const interval = setInterval(() => {
			if (isAuthenticated) {
				fetchNewMessages();
			}
		}, 3000);

		// Listen for auth events from AWS Amplify with typed events
		Hub.listen('auth', ({ payload }) => {
			const event = payload.event as AuthEvent;
			switch (event) {
				case 'signIn':
					isAuthenticated = true;
					break;
				case 'signOut':
					isAuthenticated = false;
					break;
			}
		});

		// Check initial authentication status
		try {
			const user = await checkAuth();
			if (user) {
				isAuthenticated = true;
				username = user.username || username;
			} else {
				isAuthenticated = false;
			}
		} catch (error) {
			const errorMsg = error instanceof Error ? error.message : String(error || 'Unknown error');
			const errorDetails = error && typeof error === 'object' ? JSON.stringify(error) : 'No details available';
			console.error('Auth error:', errorMsg, 'Details:', errorDetails);
			errorMessage = 'Authentication error.';
		} finally {
			authChecked = true;
		}

		// Cleanup interval on component destroy
		return () => {
			clearInterval(interval);
		};
	});

	// Reactive statement: fetch all messages when authenticated
	$: if (isAuthenticated) {
		fetchAllMessages();
	}

	// Scroll to bottom after updates
	afterUpdate(() => {
		scrollToBottom();
	});
</script>

<div class="max-w-2xl mx-auto p-4">
	<!-- Login/Logout Buttons -->
	<div class="mb-4 flex justify-end space-x-2">
		{#if authChecked}
			{#if isAuthenticated}
				<button
					on:click={handleLogout}
					class="cursor-pointer bg-red-500 hover:bg-red-600 text-white font-semibold px-4 py-2 rounded"
				>
					Logout
				</button>
			{:else}
				<button
					on:click={handleLogin}
					class="cursor-pointer bg-green-500 hover:bg-green-600 text-white font-semibold px-4 py-2 rounded"
				>
					Login with Cognito
				</button>
			{/if}
		{/if}
	</div>

	<!-- Main Content -->
	{#if authChecked}
		{#if isAuthenticated}
			{#if errorMessage}
				<div class="text-red-5 00 mb-4">{errorMessage}</div>
			{/if}
			<div class="mb-4 flex items-center">
				<label for="username" class="font-semibold mr-2">Nickname:</label>
				<input
					id="username"
					type="text"
					autocomplete="off"
					bind:value={username}
					disabled={isAuthenticated}
					class="border border-gray-300 rounded px-3 py-2"
					placeholder="Enter your nickname"
				/>
			</div>

			<h1 class="text-2xl font-bold mb-4">Chat Room</h1>

			<div
				class="border border-gray-300 rounded p-4 mb-4 h-80 overflow-y-auto"
				bind:this={chatContainer}
			>
				{#each messages as msg (msg.timestamp)}
					<div class="mb-2">
						<span class="font-semibold">{msg.username}</span>
						<span class="text-sm text-gray-500 ml-2"
						>{new Date(msg.timestamp).toLocaleTimeString()}</span
						>
						<p>{msg.message}</p>
					</div>
				{/each}
			</div>

			<div class="flex space-x-2">
				<input
					type="text"
					bind:value={newMessage}
					class="flex-grow border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring"
					placeholder="Type your message..."
					on:keydown={(e) => e.key === 'Enter' && sendMessage()}
				/>
				<button
					on:click={sendMessage}
					class="cursor-pointer bg-blue-500 hover:bg-blue-600 text-white font-semibold px-4 py-2 rounded"
				>
					Send
				</button>
			</div>
		{:else}
			<div class="text-center text-gray-600">
				<p>Please log in to access the chat.</p>
			</div>
		{/if}
	{:else}
		<div class="text-center p-4">Loading authentication status...</div>
	{/if}
</div>