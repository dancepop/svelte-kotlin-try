import { describe, expect, it, vi } from 'vitest';
import { GraphQLRequestError, graphqlRequest } from './graphql';

const URL = 'http://gateway.test/graphql';

function respond(status: number, body: unknown): typeof fetch {
	return vi.fn(
		async () => new Response(JSON.stringify(body), { status })
	) as unknown as typeof fetch;
}

describe('graphqlRequest', () => {
	it('POSTs the query and variables as JSON and returns data', async () => {
		const fetchFn = respond(200, { data: { hello: 'world' } });

		const data = await graphqlRequest<{ hello: string }>(URL, '{ hello }', { id: 1 }, fetchFn);

		expect(data).toEqual({ hello: 'world' });
		expect(fetchFn).toHaveBeenCalledWith(URL, {
			method: 'POST',
			headers: { 'content-type': 'application/json' },
			body: JSON.stringify({ query: '{ hello }', variables: { id: 1 } })
		});
	});

	it('throws all GraphQL error messages, even on HTTP 400', async () => {
		const fetchFn = respond(400, {
			errors: [{ message: 'Unknown field "x"' }, { message: 'Bad' }]
		});

		await expect(graphqlRequest(URL, '{ x }', {}, fetchFn)).rejects.toThrow(
			'Unknown field "x"; Bad'
		);
	});

	it('throws a friendly error when the gateway is unreachable', async () => {
		const fetchFn = vi.fn(async () => {
			throw new TypeError('fetch failed');
		}) as unknown as typeof fetch;

		await expect(graphqlRequest(URL, '{ hello }', {}, fetchFn)).rejects.toThrow(
			'Could not reach the gateway'
		);
	});

	it('throws the HTTP status when the body is not JSON', async () => {
		const fetchFn = vi.fn(
			async () => new Response('<html>Bad Gateway</html>', { status: 502 })
		) as unknown as typeof fetch;

		await expect(graphqlRequest(URL, '{ hello }', {}, fetchFn)).rejects.toThrow('HTTP 502');
	});

	it('throws when a successful response has no data', async () => {
		const error = await graphqlRequest(URL, '{ hello }', {}, respond(200, { data: null })).catch(
			(e: unknown) => e
		);

		expect(error).toBeInstanceOf(GraphQLRequestError);
		expect((error as Error).message).toContain('HTTP 200');
	});
});
