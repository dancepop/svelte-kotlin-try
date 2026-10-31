<script lang="ts">
	import Initials from './Initials.svelte';

	let {
		url,
		title,
		lazy = false
	}: { url: string | null; title: string; lazy?: boolean } = $props();
</script>

<!-- overflow-hidden keeps the box at 2:3: without it, a tall image makes the box grow to fit. -->
<div class="aspect-2/3 w-full overflow-hidden">
	{#if url}
		<img
			src={url}
			alt="Artwork for {title}"
			class="size-full object-cover"
			loading={lazy ? 'lazy' : 'eager'}
		/>
	{:else}
		<Initials name={title} class="size-full text-4xl" />
	{/if}
</div>
