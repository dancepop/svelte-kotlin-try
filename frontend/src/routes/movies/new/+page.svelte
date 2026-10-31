<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { createMovie } from '#lib/api/movies.ts';
	import type { MovieInput } from '#lib/api/types.ts';
	import MovieForm from '#lib/components/MovieForm.svelte';

	async function save(input: MovieInput) {
		const id = await createMovie(input);
		await goto(resolve('/movies/[id]', { id }));
	}
</script>

<svelte:head>
	<title>New movie</title>
</svelte:head>

<div class="max-w-2xl">
	<h1 class="text-3xl font-bold">New movie</h1>
	<p class="text-base-content/70 mt-1 mb-4">You can upload artwork once the movie is created.</p>
	<MovieForm submitLabel="Create movie" onsave={save} />
</div>
