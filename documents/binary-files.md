# Accessing binary files

Binary files can be accessed using HCMS API. The following example demonstrates how to access a binary file using the API.


```
<img src="assets/cricket-logo.svg" class="border rounded shadow mt-1 mb-3 p-2" width="30%" >
```
<!-- 
<img class="border rounded shadow mt-1 mb-3 p-2" width="30%" src="/api/file?path=assets/cricket-logo.svg">

<a href="/api/file?path=assets/cricket-logo.svg" target="_blank">download file from HCMS</a>
-->

<!-- links below should be transformed by HCMS on load -->
<img src="./assets/cricket-logo.svg" class="border rounded shadow mt-1 mb-3 p-2" width="30%" >

<a href="assets/cricket-logo.svg">download file</a>

The markdown image syntax can be used as well but the image size cannot be specified with this syntax. Use the HTML `<img>` tag if you need to specify the image size.

<code class="text-secondary">
![cricket-logo](assets/cricket-logo.svg)
</code>

![cricket-logo](assets/cricket-logo.svg)