FROM node:20-alpine AS builder
WORKDIR /app
ENV NODE_ENV=production
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1
COPY jira-frontend/package.json .
RUN npm install --ignore-scripts
COPY jira-frontend/ .
RUN npm run build
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY jira-frontend/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 3000
CMD ["nginx","-g","daemon off;"]
