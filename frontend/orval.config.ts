import { defineConfig } from "orval";

export default defineConfig({
  archigentn: {
    input: {
      target: "./openapi/api-docs.json",
    },
    output: {
      mode: "tags-split",
      target: "./src/api/endpoints",
      schemas: "./src/api/models",
      client: "react-query",
      httpClient: "fetch",
      baseUrl: "",
      prettier: true,
    },
  },
});
