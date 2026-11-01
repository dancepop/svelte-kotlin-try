import { GRAPHQL_URL } from '$app/env/public';
import { graphqlRequest } from './graphql.ts';
import type { ListPage, Person, PersonInput, PersonSummary } from './types.ts';

export async function listPeople(args: {
	search: string;
	page: number;
	pageSize: number;
}): Promise<ListPage<PersonSummary>> {
	const query = /* GraphQL */ `
		query People($search: String, $page: Int, $pageSize: Int) {
			people(search: $search, page: $page, pageSize: $pageSize) {
				items {
					id
					name
					bio
				}
				totalCount
			}
		}
	`;
	const { people } = await graphqlRequest<{ people: ListPage<PersonSummary> }>(
		GRAPHQL_URL,
		query,
		args
	);
	return people;
}

export async function getPerson(id: string): Promise<Person | null> {
	const query = /* GraphQL */ `
		query Person($id: ID!) {
			person(id: $id) {
				id
				name
				bio
				credits {
					movieId
					title
					type
				}
			}
		}
	`;
	const { person } = await graphqlRequest<{ person: Person | null }>(GRAPHQL_URL, query, { id });
	return person;
}

export async function createPerson(input: PersonInput): Promise<string> {
	const query = /* GraphQL */ `
		mutation CreatePerson($input: PersonInput!) {
			createPerson(input: $input) {
				id
			}
		}
	`;
	const { createPerson } = await graphqlRequest<{ createPerson: { id: string } }>(
		GRAPHQL_URL,
		query,
		{
			input
		}
	);
	return createPerson.id;
}

export async function updatePerson(id: string, input: PersonInput): Promise<void> {
	const query = /* GraphQL */ `
		mutation UpdatePerson($id: ID!, $input: PersonInput!) {
			updatePerson(id: $id, input: $input) {
				id
			}
		}
	`;
	await graphqlRequest(GRAPHQL_URL, query, { id, input });
}

export async function deletePerson(id: string): Promise<void> {
	const query = /* GraphQL */ `
		mutation DeletePerson($id: ID!) {
			deletePerson(id: $id)
		}
	`;
	await graphqlRequest(GRAPHQL_URL, query, { id });
}
