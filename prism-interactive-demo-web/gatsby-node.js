const path = require(`path`);
const { createFilePath } = require(`gatsby-source-filesystem`);

exports.createPages = async ({ graphql, actions, reporter }) => {
  const { createPage } = actions;

  // Define a template for blog post
  const blogPost = path.resolve(`./src/templates/blog-post/blog-post.js`);

  // Get all markdown blog posts sorted by date
  const result = await graphql(
    `
      {
        allMarkdownRemark(sort: { fields: [frontmatter___date], order: ASC }, limit: 1000) {
          nodes {
            id
            fields {
              slug
            }
          }
        }
      }
    `
  );

  if (result.errors) {
    reporter.panicOnBuild(`There was an error loading your blog posts`, result.errors);
    return;
  }

  const posts = result.data.allMarkdownRemark.nodes;

  // Create blog posts pages
  // But only if there's at least one markdown file found at "content/blog" (defined in gatsby-config.js)
  // `context` is available in the template as a prop and as a variable in GraphQL

  if (posts.length > 0) {
    posts.forEach((post, index) => {
      const previousPostId = index === 0 ? null : posts[index - 1].id;
      const nextPostId = index === posts.length - 1 ? null : posts[index + 1].id;

      createPage({
        path: post.fields.slug,
        component: blogPost,
        context: {
          id: post.id,
          previousPostId,
          nextPostId
        }
      });
    });
  }

  // Define a template for filter results
  const filterResults = path.resolve(`./src/templates/filter-results/filter-results.js`);

  // Get all years with published blog posts
  const yearResults = await graphql(
    `
      {
        allMarkdownRemark {
          group(field: fields___year) {
            fieldValue
          }
        }
      }
    `
  );

  if (yearResults.errors) {
    reporter.panicOnBuild(`There was an error loading your blog posts`, yearResults.errors);
    return;
  }

  const years = yearResults.data.allMarkdownRemark.group;

  years.forEach(({ fieldValue: year }) =>
    createPage({
      path: `/blog/${year}`,
      component: filterResults,
      context: { year: Number(year) }
    })
  );

  // Get all months with published blog posts for each year
  const monthResults = await graphql(
    `
      {
        allMarkdownRemark {
          group(field: fields___year_month) {
            fieldValue
          }
        }
      }
    `
  );

  if (monthResults.errors) {
    reporter.panicOnBuild(`There was an error loading your blog posts`, monthResults.errors);
    return;
  }

  const months = monthResults.data.allMarkdownRemark.group;

  months.forEach(({ fieldValue }) => {
    const [year, month] = fieldValue.split('_');
    createPage({
      path: `/blog/${year}/${month}`,
      component: filterResults,
      context: { year: Number(year), month: Number(month) }
    });
  });

  // Get all authors
  const authorResults = await graphql(
    `
      {
        allAuthorsJson {
          nodes {
            name
          }
        }
      }
    `
  );

  if (authorResults.errors) {
    reporter.panicOnBuild(`There was an error loading your blog posts`, authorResults.errors);
    return;
  }

  const authors = authorResults.data.allAuthorsJson.nodes;

  authors.forEach(({ name }) => {
    const author = name.toLowerCase().replace(' ', '-');
    createPage({
      path: `/authors/${author}`,
      component: filterResults,
      context: { authorName: name }
    });
  });
};

exports.onCreateNode = ({ node, actions, getNode }) => {
  const { createNodeField } = actions;

  if (node.internal.type === `MarkdownRemark`) {
    const date = new Date(node.frontmatter.date);
    const year = date.getFullYear();
    const month = date.getMonth() + 1;
    const slug = createFilePath({ node, getNode });

    createNodeField({ node, name: 'year', value: year });
    createNodeField({ node, name: 'month', value: month });
    createNodeField({ node, name: 'year_month', value: `${year}_${month}` });
    createNodeField({ name: `slug`, node, value: slug });
  }
};

exports.createSchemaCustomization = ({ actions }) => {
  const { createTypes } = actions;

  // Explicitly define the siteMetadata {} object
  // This way those will always be defined even if removed from gatsby-config.js

  // Also explicitly define the Markdown frontmatter
  // This way the "MarkdownRemark" queries will return `null` even when no
  // blog posts are stored inside "content/blog" instead of returning an error
  createTypes(`
    type SiteSiteMetadata {
      author: Author
      siteUrl: String
      social: Social
    }
    type Author {
      name: String
      summary: String
    }
    type Social {
      twitter: String
    }
    type MarkdownRemark implements Node {
      frontmatter: Frontmatter
      fields: Fields
    }
    type Frontmatter {
      title: String
      description: String
      date: Date @dateformat
    }
    type Fields {
      slug: String
    }
  `);
};

exports.onCreateWebpackConfig = ({ stage, loaders, actions }) => {
  if (stage === 'build-html') {
    actions.setWebpackConfig({
      module: {
        rules: [
          {
            test: /atala-prism-demo/,
            use: loaders.null()
          }
        ]
      }
    });
  }
};
