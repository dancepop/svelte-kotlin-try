<script lang="ts">
	let {
		value,
		label,
		placeholder,
		hint,
		onsearch
	}: {
		value: string;
		label: string;
		placeholder: string;
		hint?: string;
		onsearch: (text: string) => void;
	} = $props();

	// Follows `value` when the URL changes, such as on the back button, and can be typed over until then.
	let text = $derived(value);

	function submit(event: SubmitEvent) {
		event.preventDefault();
		onsearch(text.trim());
	}
</script>

<div class="mb-4">
	<form role="search" class="join w-full" onsubmit={submit}>
		<input
			type="search"
			class="input join-item w-full"
			aria-label={label}
			{placeholder}
			bind:value={text}
		/>
		<button type="submit" class="btn btn-primary join-item">Search</button>
	</form>
	{#if hint}
		<p class="text-base-content/70 mt-1 text-sm">{hint}</p>
	{/if}
</div>
