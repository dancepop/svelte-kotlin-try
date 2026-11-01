import { GRAPHQL_URL } from '$app/env/public';
import { readResponse } from './graphql.ts';

const UPLOAD_ARTWORK = /* GraphQL */ `
	mutation UploadArtwork($movieId: ID!, $file: Upload!) {
		uploadMovieArtwork(movieId: $movieId, file: $file)
	}
`;

/**
 * Sends a GraphQL multipart request and resolves with the new artwork's URL.
 * Uses XMLHttpRequest because fetch doesn't report upload progress. `onProgress` gets a fraction from 0 to 1.
 */
export function uploadArtwork(
	movieId: string,
	file: File,
	onProgress: (fraction: number) => void
): Promise<string> {
	// The file is null in `operations`; `map` tells the server which form part goes in its place.
	const form = new FormData();
	form.append(
		'operations',
		JSON.stringify({ query: UPLOAD_ARTWORK, variables: { movieId, file: null } })
	);
	form.append('map', JSON.stringify({ 0: ['variables.file'] }));
	form.append('0', file);

	return new Promise((resolve, reject) => {
		const xhr = new XMLHttpRequest();
		xhr.open('POST', GRAPHQL_URL);
		// xhr.response is the parsed body, or null if it isn't JSON.
		xhr.responseType = 'json';
		// For Apollo's default CSRF protection. Used for multipart requests.
		xhr.setRequestHeader('apollo-require-preflight', 'true');

		xhr.upload.onprogress = (event) => {
			onProgress(event.loaded / event.total);
		};
		xhr.onload = () => {
			try {
				const data = readResponse<{ uploadMovieArtwork: string }>(xhr.status, xhr.response);
				resolve(data.uploadMovieArtwork);
			} catch (error) {
				reject(error);
			}
		};
		xhr.onerror = () => reject(new Error('Could not reach the gateway. Is it running?'));
		xhr.send(form);
	});
}
