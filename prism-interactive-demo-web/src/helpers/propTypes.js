import { shape, string, number, arrayOf } from 'prop-types';

export const postShape = shape({
  id: string,
  excerpt: string,
  html: string,
  fields: shape({
    slug: string
  }),
  frontmatter: shape({
    date: string,
    title: string,
    description: string,
    author: string,
    readingTime: number,
    image: shape({
      publicURL: string
    })
  }),
  internal: shape({
    content: string
  })
});

export const postsShape = arrayOf(postShape);

const recentPostShape = shape({
  fields: shape({
    slug: string
  }),
  frontmatter: shape({
    date: string,
    title: string,
    author: string
  })
});

export const recentPostsShape = arrayOf(recentPostShape);

export const groupedPostsShape = arrayOf(
  shape({
    totalCount: number,
    fieldValue: string
  })
);

export const postLinkShape = shape({
  fields: shape({
    slug: string
  }),
  frontmatter: shape({
    title: string
  })
});

export const videoShape = shape({
  title: string,
  description: string,
  thumbnail: string,
  url: string,
  watchTime: string
});

export const brochureShape = shape({
  title: string,
  url: string,
  thumbnail: string,
  description: string
});

const authorShape = shape({
  photo: string,
  name: string,
  title: string,
  position: string,
  company: string,
  email: string,
  youtube: string,
  twitter: string,
  linkedIn: string,
  github: string
});

export const authorsShape = arrayOf(authorShape);
