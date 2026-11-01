export const PAGE_SIZE = 20;

type Params = Pick<URLSearchParams, 'get'>;

/** The search text in a list page's URL. */
export function searchTextOf(params: Params): string {
	return params.get('q') ?? '';
}

/** The page number in a list page's URL, counted from 1. No `page` means page 1. */
export function pageNumberOf(params: Params): number {
	return Number(params.get('page') ?? 1);
}

/** The query string for a list page, such as `q=heat&page=2`. */
export function listQuery(searchText: string, pageNumber: number): string {
	const params = new URLSearchParams();
	if (searchText) params.set('q', searchText);
	if (pageNumber > 1) params.set('page', String(pageNumber));
	return params.toString();
}
