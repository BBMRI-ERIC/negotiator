<template>
  <div
    class="modal"
    tabindex="-1"
    aria-hidden="true"
    data-bs-backdrop="static"
    data-bs-keyboard="true"
  >
    <div class="modal-dialog modal-dialog-centered">
      <div class="modal-content border-0 shadow-none">
        <div class="d-flex justify-content-center flex-row p-5">
          <div class="d-flex justify-content-center">
            <div class="spinner-border d-flex justify-content-center" role="status" />
            <div class="d-flex justify-content-center">
              <h4 class="mb-3 ms-3">Downloading ...</h4>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>