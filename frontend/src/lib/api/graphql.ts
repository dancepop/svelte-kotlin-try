export class GraphQLRequestError extends Error {
	override name = 'GraphQLRequestError';
}

interface GraphQLResponse<T> {
	data?: T | null;
	errors?: { message: string }[];
}

/**
 * Minimal GraphQL-over-HTTP client. The real client library (Apollo Client / urql) is chosen in the
 * design phase; this keeps the walking skeleton dependency-free.
 */
export async function graphqlRequest<T>(
	url: string,
	query: string,
	variables: Record<string, unknown> = {},
	fetchFn: typeof fetch = fetch
): Promise<T> {
	let response: Response;
	try {
		response = await fetchFn(url, {
			method: 'POST',
			headers: { 'content-type': 'application/json' },
			body: JSON.stringify({ query, variables })
		});
	} catch {
		throw new GraphQLRequestError('Could not reach the gateway. Is it running?');
	}

	const body: GraphQLResponse<T> | null = await response.json().catch(() => null);

	// GraphQL errors come first: Apollo reports invalid queries as HTTP 400 with an `errors` array.
	if (body?.errors?.length) {
		throw new GraphQLRequestError(body.errors.map((e) => e.message).join('; '));
	}
	if (!response.ok || !body?.data) {
		throw new GraphQLRequestError(`Gateway responded with HTTP ${response.status}`);
	}
	return body.data;
}
