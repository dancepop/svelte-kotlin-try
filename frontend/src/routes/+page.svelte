<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import { Loader } from '#lib/api/loader.svelte.ts';
	import { listMovies } from '#lib/api/movies.ts';
	import ErrorMessage from '#lib/components/ErrorMessage.svelte';
	import MovieCard from '#lib/components/MovieCard.svelte';
	import Pagination from '#lib/components/Pagination.svelte';
	import SearchBox from '#lib/components/SearchBox.svelte';
	import Spinner from '#lib/components/Spinner.svelte';
	import { listQuery, PAGE_SIZE, pageNumberOf, searchTextOf } from '#lib/list-url.ts';

	// A filter starts a word, so `recast:x` stays part of the title search. Its value can follow a space,
	// and is quoted or runs to the next space.
	const FILTER = /(?<!\S)(cast|creator):\s*(?:"([^"]*)"|(\S*))/gi;

	/** Splits `heat cast:david creator:"michael mann"` into its filters. If a filter is repeated, the last one wins. */
	function parseSearch(text: string): { search: string; castName: string; creatorName: string } {
		let castName = '';
		let creatorName = '';
		const search = text
			.replace(FILTER, (_match, filter: string, quoted?: string, plain?: string) => {
				const value = quoted ?? plain ?? '';
				if (filter.toLowerCase() === 'cast') castName = value;
				else creatorName = value;
				return '';
			})
			.replace(/\s+/g, ' ')
			.trim();
		return { search, castName, creatorName };
	}

	const searchText = $derived(searchTextOf(page.url.searchParams));
	const pageNumber = $derived(pageNumberOf(page.url.searchParams));

	const movies = new Loader(() =>
		listMovies({ ...parseSearch(searchText), page: pageNumber - 1, pageSize: PAGE_SIZE })
	);

	function show(text: string, number: number) {
		void goto(resolve(`/?${listQuery(text, number)}`));
	}
</script>

<svelte:head>
	<title>Movies</title>
</svelte:head>

<div class="mb-4 flex flex-wrap items-center justify-between gap-3">
	<h1 class="text-3xl font-bold">Movies</h1>
	<a class="btn btn-primary" href={resolve('/movies/new')}>New movie</a>
</div>

<SearchBox
	value={searchText}
	label="Search movies"
	placeholder="heat cast:david creator:&quot;michael mann&quot;"
	hint="Searches titles. Use cast: and creator: to search by name, with quotes around full names."
	onsearch={(text) => show(text, 1)}
/>

{#if movies.error}
	<ErrorMessage message={movies.error} onretry={() => movies.reload()} />
{:else if movies.data}
	{#if movies.loading}
		<Spinner />
	{/if}
	<div class:opacity-50={movies.loading}>
		{#if movies.data.items.length === 0}
			<p class="text-base-content/70">No movies found</p>
		{:else}
			<div class="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
				{#each movies.data.items as movie (movie.id)}
					<MovieCard {movie} />
				{/each}
			</div>
		{/if}
		<Pagination
			{pageNumber}
			pageSize={PAGE_SIZE}
			totalCount={movies.data.totalCount}
			onchange={(number) => show(searchText, number)}
		/>
	</div>
{:else}
	<Spinner />
{/if}
