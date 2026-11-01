export interface GraphQLResponse<T> {
	data?: T | null;
	errors?: { message: string }[];
}

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
		throw new Error('Could not reach the gateway. Is it running?');
	}

	const body: GraphQLResponse<T> = await response.json();
	return readResponse(response.status, body);
}

export function readResponse<T>(status: number, response: GraphQLResponse<T> | null): T {
	// Not entirely sure about the conventions surrounding graphql returning 200 with validation errors
	// For this case its 200 with validation errors hence we check for errors first.
	if (response?.errors?.length) {
		throw new Error(response.errors.map((e) => e.message).join('; '));
	}
	if (status < 200 || status >= 300 || !response?.data) {
		throw new Error(`Gateway responded with HTTP ${status}`);
	}
	return response.data;
}

export function errorMessage(error: unknown): string {
	return (error as Error).message;
}
