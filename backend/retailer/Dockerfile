FROM node:22-alpine

WORKDIR /app

COPY package.json ./
COPY src ./src
COPY scripts ./scripts

ENV PORT=8787
EXPOSE 8787

CMD ["npm", "start"]
