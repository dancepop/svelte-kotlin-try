<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import { errorMessage } from '#lib/api/graphql.ts';
	import { Loader } from '#lib/api/loader.svelte.ts';
	import { deleteMovie, getMovie, updateMovie } from '#lib/api/movies.ts';
	import type { MovieInput } from '#lib/api/types.ts';
	import Artwork from '#lib/components/Artwork.svelte';
	import ArtworkUpload from '#lib/components/ArtworkUpload.svelte';
	import CreditList from '#lib/components/CreditList.svelte';
	import ErrorMessage from '#lib/components/ErrorMessage.svelte';
	import MovieForm from '#lib/components/MovieForm.svelte';
	import Spinner from '#lib/components/Spinner.svelte';

	const id = $derived(page.params.id as string);
	const movie = new Loader(() => getMovie(id));

	let editing = $state(false);
	let deleting = $state(false);
	let deleteError = $state<string | null>(null);

	const facts = $derived(
		[
			movie.data?.releaseDate,
			movie.data?.runtimeMinutes ? `${movie.data.runtimeMinutes} min` : null
		]
			.filter(Boolean)
			.join(' · ')
	);

	async function save(input: MovieInput) {
		await updateMovie(id, input);
		await movie.reload();
		editing = false;
	}

	async function remove() {
		if (!confirm(`Delete "${movie.data?.title}"?`)) return;
		deleting = true;
		deleteError = null;
		try {
			await deleteMovie(id);
			await goto(resolve('/'));
		} catch (error) {
			deleteError = errorMessage(error);
			deleting = false;
		}
	}

	function showArtwork(artworkUrl: string) {
		if (movie.data) movie.data = { ...movie.data, artworkUrl };
	}
</script>

<svelte:head>
	<title>{movie.data?.title ?? 'Movie'}</title>
</svelte:head>

{#if movie.error}
	<ErrorMessage message={movie.error} onretry={() => movie.reload()} />
{:else if movie.data}
	<div class="grid gap-6 md:grid-cols-[280px_1fr]">
		<div>
			<div class="rounded-box overflow-hidden shadow-md">
				<Artwork url={movie.data.artworkUrl} title={movie.data.title} />
			</div>
			<ArtworkUpload movieId={id} onuploaded={showArtwork} />
		</div>

		<div class="min-w-0">
			{#if deleteError}
				<p class="alert alert-error mb-4" role="alert">{deleteError}</p>
			{/if}

			{#if editing}
				<MovieForm
					movie={movie.data}
					submitLabel="Save changes"
					onsave={save}
					oncancel={() => (editing = false)}
				/>
			{:else}
				<div class="flex flex-wrap items-center justify-between gap-3">
					<h1 class="text-3xl font-bold wrap-anywhere">{movie.data.title}</h1>
					<div class="flex gap-2">
						<button type="button" class="btn" onclick={() => (editing = true)}>Edit</button>
						<button
							type="button"
							class="btn btn-error btn-outline"
							disabled={deleting}
							onclick={remove}
						>
							{deleting ? 'Deleting…' : 'Delete'}
						</button>
					</div>
				</div>
				{#if facts}
					<p class="text-base-content/70 mt-1">{facts}</p>
				{/if}
				{#if movie.data.synopsis}
					<p class="mt-3 whitespace-pre-line">{movie.data.synopsis}</p>
				{/if}
			{/if}

			<CreditList
				movieId={id}
				type="CAST"
				heading="Cast"
				credits={movie.data.credits.filter((credit) => credit.type === 'CAST')}
				onchanged={() => movie.reload()}
			/>
			<CreditList
				movieId={id}
				type="CREATOR"
				heading="Creators"
				credits={movie.data.credits.filter((credit) => credit.type === 'CREATOR')}
				onchanged={() => movie.reload()}
			/>
		</div>
	</div>
{:else if movie.loading}
	<Spinner />
{:else}
	<p class="text-base-content/70">
		Movie not found. <a class="link" href={resolve('/')}>Back to movies</a>
	</p>
{/if}
