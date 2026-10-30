import { defineEnvVars } from '@sveltejs/kit/env';

export const variables = defineEnvVars({
	GRAPHQL_URL: {
		public: true,
		// Read when the server starts, not inlined at build time.
		static: false,
		description: 'URL of the GraphQL gateway, as seen from the browser.',
		schema: (value) => value || 'http://localhost:4000/graphql'
	}
});
