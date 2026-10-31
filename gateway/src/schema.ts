export const typeDefs = /* GraphQL */ `
  scalar Upload

  type Query {
    movies(search: String, castName: String, creatorName: String, page: Int = 0, pageSize: Int = 20): MoviePage!
    movie(id: ID!): Movie
    people(search: String, page: Int = 0, pageSize: Int = 20): PersonPage!
    person(id: ID!): Person
  }

  type Mutation {
    createMovie(input: MovieInput!): Movie!
    updateMovie(id: ID!, input: MovieInput!): Movie!
    deleteMovie(id: ID!): Boolean!
    "Returns the new artwork's URL."
    uploadMovieArtwork(movieId: ID!, file: Upload!): String!
    createPerson(input: PersonInput!): Person!
    updatePerson(id: ID!, input: PersonInput!): Person!
    deletePerson(id: ID!): Boolean!
    addCredit(movieId: ID!, personId: ID!, type: CreditType!): Boolean!
    removeCredit(movieId: ID!, personId: ID!, type: CreditType!): Boolean!
  }

  input MovieInput {
    title: String!
    synopsis: String
    releaseDate: String
    runtimeMinutes: Int
  }

  input PersonInput {
    name: String!
    bio: String
  }

  enum CreditType {
    CAST
    CREATOR
  }

  type MoviePage {
    items: [Movie!]!
    totalCount: Int!
  }

  type PersonPage {
    items: [Person!]!
    totalCount: Int!
  }

  type Movie {
    id: ID!
    title: String!
    synopsis: String
    releaseDate: String
    runtimeMinutes: Int
    artworkUrl: String
    credits: [MovieCredit!]!
    createdAt: String!
    updatedAt: String!
  }

  type MovieCredit {
    personId: ID!
    name: String!
    type: CreditType!
  }

  type Person {
    id: ID!
    name: String!
    bio: String
    credits: [PersonCredit!]!
    createdAt: String!
    updatedAt: String!
  }

  type PersonCredit {
    movieId: ID!
    title: String!
    type: CreditType!
  }
`;
