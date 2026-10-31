export type CreditType = 'CAST' | 'CREATOR';

/** What the movie grid shows. */
export interface MovieSummary {
	id: string;
	title: string;
	releaseDate: string | null;
	artworkUrl: string | null;
}

export interface MovieCredit {
	personId: string;
	name: string;
	type: CreditType;
}

export interface Movie extends MovieSummary {
	synopsis: string | null;
	runtimeMinutes: number | null;
	credits: MovieCredit[];
}

export interface MovieInput {
	title: string;
	synopsis: string | null;
	releaseDate: string | null;
	runtimeMinutes: number | null;
}

/** What the people list shows. */
export interface PersonSummary {
	id: string;
	name: string;
	bio: string | null;
}

export interface PersonCredit {
	movieId: string;
	title: string;
	type: CreditType;
}

export interface Person extends PersonSummary {
	credits: PersonCredit[];
}

export interface PersonInput {
	name: string;
	bio: string | null;
}

export interface ListPage<T> {
	items: T[];
	totalCount: number;
}
