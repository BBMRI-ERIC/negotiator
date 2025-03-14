<template>
    <span
      class="avatar"
      :style="{ 'background-color': uiConfiguration?.navbarButtonOutlineColor + '!important' }"
    >
      <slot></slot
    ></span>
  </template>
  
  <style scoped>
  .avatar {
    color: #fff;
    padding: 2px;
    display: flex;
    justify-content: center;
    align-items: center;
    border-radius: 50%;
    width: 40px;
    height: 40px;
    font-weight: 400;
  }
</style>