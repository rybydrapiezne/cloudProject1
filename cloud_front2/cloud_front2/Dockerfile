# Use a Node.js base image
FROM node:18-alpine

# Set working directory
WORKDIR /app

# Copy package files first for better caching
COPY package.json package-lock.json ./

# Install dependencies
RUN npm install --frozen-lockfile

# Copy all source files
COPY . .

# Build the application
RUN npm run build

# Expose the correct port
EXPOSE 5173

# Command to run the application
CMD ["npm", "run", "preview", "--", "--port", "5173", "--host"]
