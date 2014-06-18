# yahaml4j - Yet Another Haml for Java.

This is a direct port of the [clientside-haml-js](https://github.com/uglyog/clientside-haml-js) HAML compiler from Coffeescript/Javascript to the JVM.

## OMG! Why?

I was on a project that required converting JSPs to another templating language, and after looking at Velocity and FreeMarker, I thought "What about HAML?". So I had a look at the few HAML JVM versions out there, and felt they did not quite meet my requirements. This was the same reason I wrote the Javascript version.

## My requirements

1. Be fully HAML complaint as the target language allows (this means implement all the features in the HAML spec)
2. Able to render a HAML template
3. Able to generate a Java class from a HAML template that can render that template
4. Give really good feedback when a template can't be rendered

## Contributing

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create new Pull Request

