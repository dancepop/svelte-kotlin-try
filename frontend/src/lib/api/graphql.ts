export class GraphQLRequestError extends Error {
	override name = 'GraphQLRequestError';
	readonly code?: string;

	constructor(message: string, code?: string) {
		super(message);
		this.code = code;
	}
}

export interface GraphQLResponse<T> {
	data?: T | null;
	errors?: {
		message: string;
		extensions?: { code?: string };
	}[];
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
		throw new GraphQLRequestError('Could not reach the gateway. Is it running?');
	}

	const body: GraphQLResponse<T> | null = await response.json().catch(() => null);
	return readResponse(response.status, body);
}

export function readResponse<T>(status: number, response: GraphQLResponse<T> | null): T {
	// Not entirely sure about the conventions surrounding graphql returning 200 with validation errors
	// For this case its 200 with validation errors hence we check for errors first.
	if (response?.errors?.length) {
		const { code } = response.errors[0].extensions ?? {};
		throw new GraphQLRequestError(response.errors.map((e) => e.message).join('; '), code);
	}
	if (status < 200 || status >= 300 || !response?.data) {
		throw new GraphQLRequestError(`Gateway responded with HTTP ${status}`);
	}
	return response.data;
}

export function errorMessage(error: unknown): string {
	return error instanceof Error ? error.message : 'Something went wrong, try again';
}
