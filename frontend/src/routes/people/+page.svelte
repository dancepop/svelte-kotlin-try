<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import { Loader } from '#lib/api/loader.svelte.ts';
	import { listPeople } from '#lib/api/people.ts';
	import ErrorMessage from '#lib/components/ErrorMessage.svelte';
	import Initials from '#lib/components/Initials.svelte';
	import Pagination from '#lib/components/Pagination.svelte';
	import SearchBox from '#lib/components/SearchBox.svelte';
	import Spinner from '#lib/components/Spinner.svelte';
	import { listQuery, PAGE_SIZE, pageNumberOf, searchTextOf } from '#lib/list-url.ts';

	const searchText = $derived(searchTextOf(page.url.searchParams));
	const pageNumber = $derived(pageNumberOf(page.url.searchParams));

	const people = new Loader(() =>
		listPeople({ search: searchText, page: pageNumber - 1, pageSize: PAGE_SIZE })
	);

	function show(text: string, number: number) {
		void goto(resolve(`/people?${listQuery(text, number)}`));
	}
</script>

<svelte:head>
	<title>Cast &amp; creators</title>
</svelte:head>

<div class="mb-4 flex flex-wrap items-center justify-between gap-3">
	<h1 class="text-3xl font-bold">Cast &amp; creators</h1>
	<a class="btn btn-primary" href={resolve('/people/new')}>New person</a>
</div>

<SearchBox
	value={searchText}
	label="Search people"
	placeholder="Search by name"
	onsearch={(text) => show(text, 1)}
/>

{#if people.error}
	<ErrorMessage message={people.error} onretry={() => people.reload()} />
{:else if people.data}
	{#if people.loading}
		<Spinner />
	{/if}
	<div class:opacity-50={people.loading}>
		{#if people.data.items.length === 0}
			<p class="text-base-content/70">No people found</p>
		{:else}
			<ul class="grid gap-3 md:grid-cols-2">
				{#each people.data.items as person (person.id)}
					<li>
						<a
							href={resolve('/people/[id]', { id: person.id })}
							class="rounded-box bg-base-200 flex h-full items-center gap-3 p-3 shadow-sm transition hover:shadow-lg"
						>
							<Initials name={person.name} class="size-12 shrink-0 rounded-full" />
							<div class="min-w-0">
								<strong class="block wrap-anywhere">{person.name}</strong>
								<p class="text-base-content/70 line-clamp-2 text-sm">
									{person.bio || 'No bio'}
								</p>
							</div>
						</a>
					</li>
				{/each}
			</ul>
		{/if}
		<Pagination
			{pageNumber}
			pageSize={PAGE_SIZE}
			totalCount={people.data.totalCount}
			onchange={(number) => show(searchText, number)}
		/>
	</div>
{:else}
	<Spinner />
{/if}
