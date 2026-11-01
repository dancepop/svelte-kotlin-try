import type { CodegenConfig } from "@graphql-codegen/cli";

// Generates the `Resolvers` type from the schema, so resolver arguments and return values are checked.
// Paths inside `config` are relative to the generated file.
const config: CodegenConfig = {
  schema: "src/graphql/schema.graphql",
  generates: {
    "src/graphql/generated/resolvers.ts": {
      plugins: ["typescript", "typescript-resolvers"],
      config: {
        useTypeImports: true,
        enumsAsTypes: true,
        contextType: "../context.js#Context",
        // What a type's resolvers receive as their parent. Movie and Person are built without `credits`,
        // which have their own resolvers; credits are passed through from gRPC as they are.
        mappers: {
          Movie: "../mappers.js#MovieModel",
          Person: "../mappers.js#PersonModel",
          MovieCredit: "../../grpc/generated/movie/MovieCredit.js#MovieCredit__Output",
          PersonCredit: "../../grpc/generated/movie/PersonCredit.js#PersonCredit__Output",
        },
        scalars: {
          Upload: { input: "Promise<import('graphql-upload-minimal').FileUpload>", output: "never" },
        },
      },
    },
  },
};

export default config;
