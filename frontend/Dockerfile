# Build stage
FROM node:20-alpine as build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
# You can pass REACT_APP_API_URL during build time if deploying to production
RUN npm run build

# Serve stage using Nginx
FROM nginx:alpine
# Copy the built React app to Nginx's web root
COPY --from=build /app/build /usr/share/nginx/html
# Expose port 80
EXPOSE 80
# Start Nginx
CMD ["nginx", "-g", "daemon off;"]
