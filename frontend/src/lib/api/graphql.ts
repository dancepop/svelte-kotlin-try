export interface FieldError {
	field: string;
	message: string;
}

export class GraphQLRequestError extends Error {
	override name = 'GraphQLRequestError';
	readonly code?: string;
	readonly fieldErrors: FieldError[];

	constructor(message: string, code?: string, fieldErrors: FieldError[] = []) {
		super(message);
		this.code = code;
		this.fieldErrors = fieldErrors;
	}
}

export interface GraphQLResponse<T> {
	data?: T | null;
	errors?: {
		message: string;
		extensions?: { code?: string; fieldErrors?: FieldError[] };
	}[];
}

/** Sends one GraphQL operation over HTTP. */
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

/** Returns the response's data, or throws its error. Uploads use this too, because they arrive the same way. */
export function readResponse<T>(status: number, body: GraphQLResponse<T> | null): T {
	// GraphQL errors come first: Apollo reports invalid queries as HTTP 400 with an `errors` array.
	if (body?.errors?.length) {
		const { code, fieldErrors } = body.errors[0].extensions ?? {};
		throw new GraphQLRequestError(body.errors.map((e) => e.message).join('; '), code, fieldErrors);
	}
	if (status < 200 || status >= 300 || !body?.data) {
		throw new GraphQLRequestError(`Gateway responded with HTTP ${status}`);
	}
	return body.data;
}

export function errorMessage(error: unknown): string {
	return error instanceof Error ? error.message : 'Something went wrong, try again';
}

/**
 * Splits a failed save into messages for the form's fields and a banner for the rest.
 * The banner also covers a rule on something the form has no field for, such as a malformed id.
 */
export function formErrors(
	error: unknown,
	formFields: string[]
): { fields: Record<string, string>; banner: string | null } {
	const fieldErrors = error instanceof GraphQLRequestError ? error.fieldErrors : [];
	const shown = fieldErrors.filter(({ field }) => formFields.includes(field));
	return {
		fields: Object.fromEntries(shown.map(({ field, message }) => [field, message])),
		banner: shown.length > 0 ? null : errorMessage(error)
	};
}
