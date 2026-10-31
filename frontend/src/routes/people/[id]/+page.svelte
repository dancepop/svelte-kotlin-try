<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import { errorMessage } from '#lib/api/graphql.ts';
	import { Loader } from '#lib/api/loader.svelte.ts';
	import { deletePerson, getPerson, updatePerson } from '#lib/api/people.ts';
	import type { PersonInput } from '#lib/api/types.ts';
	import ErrorMessage from '#lib/components/ErrorMessage.svelte';
	import Initials from '#lib/components/Initials.svelte';
	import PersonForm from '#lib/components/PersonForm.svelte';
	import Spinner from '#lib/components/Spinner.svelte';

	const id = $derived(page.params.id as string);
	const person = new Loader(() => getPerson(id));

	let editing = $state(false);
	let deleting = $state(false);
	let deleteError = $state<string | null>(null);

	async function save(input: PersonInput) {
		await updatePerson(id, input);
		await person.reload();
		editing = false;
	}

	async function remove() {
		if (!confirm(`Delete ${person.data?.name}? They'll also be removed from their movies.`)) return;
		deleting = true;
		deleteError = null;
		try {
			await deletePerson(id);
			await goto(resolve('/people'));
		} catch (error) {
			deleteError = errorMessage(error);
			deleting = false;
		}
	}
</script>

<svelte:head>
	<title>{person.data?.name ?? 'Person'}</title>
</svelte:head>

{#if person.error}
	<ErrorMessage message={person.error} onretry={() => person.reload()} />
{:else if person.data}
	<div class="max-w-2xl">
		{#if deleteError}
			<p class="alert alert-error mb-4" role="alert">{deleteError}</p>
		{/if}

		{#if editing}
			<PersonForm
				person={person.data}
				submitLabel="Save changes"
				onsave={save}
				oncancel={() => (editing = false)}
			/>
		{:else}
			<div class="flex flex-wrap items-center gap-3">
				<Initials name={person.data.name} class="size-16 shrink-0 rounded-full text-xl" />
				<h1 class="mr-auto text-3xl font-bold wrap-anywhere">{person.data.name}</h1>
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
			{#if person.data.bio}
				<p class="mt-3 whitespace-pre-line wrap-anywhere">{person.data.bio}</p>
			{:else}
				<p class="text-base-content/70 mt-3">No bio</p>
			{/if}
		{/if}

		<section class="rounded-box bg-base-200 mt-6 p-4">
			<h2 class="mb-2 text-lg font-semibold">Movies</h2>
			{#if person.data.credits.length > 0}
				<ul>
					{#each person.data.credits as credit (`${credit.movieId}-${credit.type}`)}
						<li class="flex items-center justify-between gap-2 py-1">
							<a
								class="link link-hover wrap-anywhere"
								href={resolve('/movies/[id]', { id: credit.movieId })}
							>
								{credit.title}
							</a>
							<span class="badge badge-soft">{credit.type === 'CAST' ? 'Cast' : 'Creator'}</span>
						</li>
					{/each}
				</ul>
			{:else}
				<p class="text-base-content/70">Not credited on any movie yet</p>
			{/if}
		</section>
	</div>
{:else if person.loading}
	<Spinner />
{:else}
	<p class="text-base-content/70">
		Person not found. <a class="link" href={resolve('/people')}>Back to cast &amp; creators</a>
	</p>
{/if}
