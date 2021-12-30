# Style Guide

This is a minimal style guide intended to maintain consistency of code elements 
that might not be enforced with tools like Eslint and Prettier. 

## Table of contents

* [Unused parameters and variables](#unused-parameters-and-variables)
* [Component events](#component-events)
* [Using the spread operator](#using-the-spread-operator)
* [Template literals](#template-literals)
* [Type coercion](#type-coercion)
* [Async code](#async-code)
* [Module exports](#module-exports)

## Unused parameters and variables
In some rare situations it would be actually convenient to have unused variables left in the code. One of such cases is unused variables when destructuring arrays.

Unused variables should be prefixed with `_`, for example: 

`const [_prefix, iconData] = values.icon.split(',');`

Following ESLint rule should be in place to support it:

`"no-unused-vars": ["warn", { "varsIgnorePattern": "^_" }]`

## Component events

Component props which are handlers (or a function in general), should be treated as events and follow its naming convention, for example:

`onClick`, `onChange`, `onSomethingChange`, `onSort`, etc.

Bad:

```js
<CustomDateRangePicker setStartDate={setStartDate} setEndDate={setEndDate} />
```

Good:

```js
<CustomDateRangePicker onStartDateChange={handleStartDateChange} onEndDateChange={setEndDate} />
```

or even:

```js
<CustomDateRangePicker onChange={({startDate, endDate}) => {/* handle dates change */} } />
```

## Using the spread operator

Use the spread operator to:
* copy objects and arrays
* to copy objects and merge new props into it (prefer the spread over the `Object.assign`)

```javascript
// copy array
let newArray = [...someArray]; // make sure that someArray is not null/undefined!

// copy objects
let newObject = {...someObject};
let newObject = {...someObject, merge: 'new props'};


```

## Template literals

Prefer using the template literals instead of concatenating strings.

## Type coercion
>From https://google.github.io/styleguide/tsguide.html#type-coercion

Use the `String()` and `Boolean()` (note: no `new`!) functions, string template literals, or `!!` to coerce types.

```javascript
const bool = Boolean(false);
const str = String(aNumber);
const bool2 = !!str;
const str2 = `result: ${bool2}`;
```

Code must use `Number()` to parse numeric values, and **must** check its return for `NaN` values explicitly, unless failing to parse is impossible from context.
>Note: `Number('')`, `Number(' ')`, and `Number('\t')` would return `0` instead of `NaN`. `Number('Infinity')` and `Number('-Infinity')` would return `Infinity` and `-Infinity` respectively. These cases may require special handling.

```javascript
const aNumber = Number('123');
if (isNaN(aNumber)) throw new Error(...);  // Handle NaN if the string might not contain a number
assertFinite(aNumber, ...);                // Optional: if NaN cannot happen because it was validated before.
```

Code must not use unary plus (`+`) to coerce strings to numbers.

Code must also not use `parseInt` or `parseFloat` to parse numbers, except for non-base-10 strings.

Use `Number()` followed by `Math.floor` or `Math.trunc` (where available) to parse integer numbers:
```javascript
let f = Number(someString);
if (isNaN(f)) handleError();
f = Math.floor(f);
```

## Async code

Prefer `async function` to `Promise`.

In MobX stores, use `flow`/`generators` instead of `async function`. `flow` doesn't require wrapping state mutations in `runInAction`! 

## Module exports

Prefer named exports to default exports. This ensures that all imports follow a uniform pattern.

More info [here](https://google.github.io/styleguide/tsguide.html#exports).
