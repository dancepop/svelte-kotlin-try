import { GRAPHQL_URL } from '$app/env/public';
import { graphqlRequest } from './graphql.ts';
import type { CreditType, ListPage, Movie, MovieInput, MovieSummary } from './types.ts';

const MOVIE_FIELDS = /* GraphQL */ `
	fragment MovieFields on Movie {
		id
		title
		synopsis
		releaseDate
		runtimeMinutes
		artworkUrl
		credits {
			personId
			name
			type
		}
	}
`;

export async function listMovies(args: {
	search: string;
	castName: string;
	creatorName: string;
	page: number;
	pageSize: number;
}): Promise<ListPage<MovieSummary>> {
	const query = /* GraphQL */ `
		query Movies(
			$search: String
			$castName: String
			$creatorName: String
			$page: Int
			$pageSize: Int
		) {
			movies(
				search: $search
				castName: $castName
				creatorName: $creatorName
				page: $page
				pageSize: $pageSize
			) {
				items {
					id
					title
					releaseDate
					artworkUrl
				}
				totalCount
			}
		}
	`;
	const { movies } = await graphqlRequest<{ movies: ListPage<MovieSummary> }>(
		GRAPHQL_URL,
		query,
		args
	);
	return movies;
}

/** Returns null for an unknown id. */
export async function getMovie(id: string): Promise<Movie | null> {
	const query = /* GraphQL */ `
		query Movie($id: ID!) {
			movie(id: $id) {
				...MovieFields
			}
		}
		${MOVIE_FIELDS}
	`;
	const { movie } = await graphqlRequest<{ movie: Movie | null }>(GRAPHQL_URL, query, { id });
	return movie;
}

export async function createMovie(input: MovieInput): Promise<string> {
	const query = /* GraphQL */ `
		mutation CreateMovie($input: MovieInput!) {
			createMovie(input: $input) {
				id
			}
		}
	`;
	const { createMovie } = await graphqlRequest<{ createMovie: { id: string } }>(
		GRAPHQL_URL,
		query,
		{
			input
		}
	);
	return createMovie.id;
}

export async function updateMovie(id: string, input: MovieInput): Promise<void> {
	const query = /* GraphQL */ `
		mutation UpdateMovie($id: ID!, $input: MovieInput!) {
			updateMovie(id: $id, input: $input) {
				id
			}
		}
	`;
	await graphqlRequest(GRAPHQL_URL, query, { id, input });
}

export async function deleteMovie(id: string): Promise<void> {
	const query = /* GraphQL */ `
		mutation DeleteMovie($id: ID!) {
			deleteMovie(id: $id)
		}
	`;
	await graphqlRequest(GRAPHQL_URL, query, { id });
}

export async function addCredit(
	movieId: string,
	personId: string,
	type: CreditType
): Promise<void> {
	const query = /* GraphQL */ `
		mutation AddCredit($movieId: ID!, $personId: ID!, $type: CreditType!) {
			addCredit(movieId: $movieId, personId: $personId, type: $type)
		}
	`;
	await graphqlRequest(GRAPHQL_URL, query, { movieId, personId, type });
}

export async function removeCredit(
	movieId: string,
	personId: string,
	type: CreditType
): Promise<void> {
	const query = /* GraphQL */ `
		mutation RemoveCredit($movieId: ID!, $personId: ID!, $type: CreditType!) {
			removeCredit(movieId: $movieId, personId: $personId, type: $type)
		}
	`;
	await graphqlRequest(GRAPHQL_URL, query, { movieId, personId, type });
}
