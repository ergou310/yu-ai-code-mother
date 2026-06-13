<script setup lang="ts">
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'
import { RouterView } from 'vue-router'
</script>

<template>
  <a-layout>
    <GlobalHeader />
    <a-layout-content>
      <RouterView />
    </a-layout-content>
    <GlobalFooter />
  </a-layout>
</template>
