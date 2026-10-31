<script lang="ts">
	let { name, class: className = '' }: { name: string; class?: string } = $props();

	const initials = $derived(
		name
			.split(/\s+/)
			.slice(0, 2)
			.map((word) => word.charAt(0))
			.join('')
			.toUpperCase()
	);

	// Worked out from the name, so the same name always gets the same colour.
	const hue = $derived(
		[...name].reduce((total, letter) => (total * 31 + letter.charCodeAt(0)) % 360, 0)
	);
</script>

<div
	class="flex items-center justify-center font-bold text-white {className}"
	style="background: linear-gradient(135deg, hsl({hue} 55% 38%), hsl({hue + 40} 55% 22%))"
	aria-hidden="true"
>
	{initials}
</div>
