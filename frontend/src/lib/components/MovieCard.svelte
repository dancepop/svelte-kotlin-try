<script lang="ts">
	import { resolve } from '$app/paths';
	import type { MovieSummary } from '#lib/api/types.ts';
	import Artwork from './Artwork.svelte';

	let { movie }: { movie: MovieSummary } = $props();
</script>

<a
	href={resolve('/movies/[id]', { id: movie.id })}
	class="card bg-base-200 overflow-hidden shadow-md transition hover:-translate-y-1 hover:shadow-xl"
>
	<Artwork url={movie.artworkUrl} title={movie.title} lazy />
	<div class="p-3">
		<strong class="block wrap-anywhere">{movie.title}</strong>
		{#if movie.releaseDate}
			<span class="text-base-content/70 text-sm">{movie.releaseDate.slice(0, 4)}</span>
		{/if}
	</div>
</a>
