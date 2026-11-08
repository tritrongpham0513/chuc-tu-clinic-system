import client from './client'

export const getPublishedArticles = (params) =>
  client.get('/articles', { params })

export const getArticleCategories = () =>
  client.get('/articles/categories')

export const getArticleBySlug = (slug) =>
  client.get(`/articles/${slug}`)
