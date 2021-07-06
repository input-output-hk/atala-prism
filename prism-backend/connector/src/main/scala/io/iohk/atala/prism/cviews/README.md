# Credential View Templates

Credential view templates (in HTML) are currently hard coded for Atala PRISM,
but work is being done to have them stored as a service. These instructions are
meant to help adding and updating hardcoded templates until the new dynamic
service starts to be used instead.

## Adding a new template

Once the design for a new template is finalized:

  1. Add the final `.html` file, which is tested against the
     [Twirl](https://github.com/playframework/twirl) template output:
      1. Copy the `.html` file into the
         [templates](https://github.com/input-output-hk/atala/tree/develop/prism-backend/connector/src/test/resources/cviews/templates) directory.
      2. Rename the file to match the existing naming convention, i.e.,
         `<deployment>_<credential_type>.html` (snake case).
      3. Change the fake data from the template to the `{{variable}}` format
         needed by the Management Console Web app.
      4. Forget about images for now, as they need to be encoded.
  2. Add the Twirl template:
      1. Copy the `.html` file from above to the
         [intdemo](https://github.com/input-output-hk/atala/tree/develop/prism-backend/connector/src/main/twirl/io/iohk/atala/prism/intdemo)
         directory.
      2. Rename the new file, along its extension, to match the convention:
         `<Deployment><CredentialType>.scala.html` (camel case).
      3. Add the headers needed for Twirl to work. See examples in the same
         directory.
      4. Replace the variables in the `{{variable}}` format to use Twirl,
         e.g., `@root.degreeName.string.getOption(credential)`.
      5. Replace image sources with `@imageSource("<image_file>")`.
         `@imageSource` will read the image file and return a
         [data URL](https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/Data_URIs)
         that can be embedded in the template output.
      6. Copy the images needed into the
         [images](https://github.com/input-output-hk/atala/tree/develop/prism-backend/connector/src/main/resources/cvp/view/images)
         directory. This is where `@imageSource` will take the image from.
  3. Return the new template from the `CredentialViewsService`:
      1. Modify the `PredefinedHtmlTemplates` object in
         [CredentialViewsService](https://github.com/input-output-hk/atala/blob/develop/prism-backend/connector/src/main/scala/io/iohk/atala/prism/cviews/CredentialViewsService.scala)
         to return the new template, by calling the Twirl template with the
         `{{variable}}` variables you expect in the resulting `.html` file.
      2. Update the `CredentialViewsServiceSpec` test accordingly.
      3. Tests will now break, as you need to get the encoded images skipped
         earlier in the process and go back to the `.html` file and set it to
         the expected value you see in the test. The test will generate a
         temporary file of the obtained template, which you can compare to the
         expected template easily with your favorite diff tool.
      4. Once tests are passing, open the `.html` file in your browser and
         manually verify it looks as expected. This will test images are
         properly displayed.

For all the above, you can find examples in the same directories you modify.

## Updating an existing template

If you are only going to slightly update a template, it may be easier to just
re-apply all changes needed. A simple way to find out those changes is to use
your favorite diff tool. 

If you notice the template change is big, you may be better off by starting over
or, in other words, following the steps listed above for adding a new template.

Once you have updated the HTML file, remember to update the `.scala.html` file
as well. Unit tests will ensure the template does produce the expected `html`
file.
