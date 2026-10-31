<script lang="ts">
	import '../app.css';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import favicon from '#lib/assets/favicon.svg';
	import type { LayoutProps } from './$types';

	let { children }: LayoutProps = $props();

	const onPeople = $derived(page.url.pathname.startsWith('/people'));
</script>

<svelte:head>
	<link rel="icon" href={favicon} />
</svelte:head>

<header class="bg-base-200 shadow-sm">
	<nav aria-label="Main" class="navbar mx-auto max-w-6xl gap-2 px-4">
		<a href={resolve('/')} class="mr-auto hidden text-xl font-bold sm:block">Movie Catalogue</a>
		<a href={resolve('/')} class="btn btn-ghost" class:btn-active={!onPeople}>Movies</a>
		<a href={resolve('/people')} class="btn btn-ghost" class:btn-active={onPeople}>
			Cast &amp; creators
		</a>
	</nav>
</header>

<main class="mx-auto max-w-6xl px-4 py-6">
	{@render children()}
</main>
