import objectMapper from 'object-mapper';

// Define types for our source object
interface Contact {
  type: string;
  value: string;
}

interface Project {
  name: string;
  status: string;
}

interface SourceObject {
  name: string;
  age: number;
  contacts: Contact[];
  skills: string[];
  projects: Project[];
}

// Source object
const source: SourceObject = {
  name: "John Doe",
  age: 30,
  contacts: [
    { type: "email", value: "john@example.com" },
    { type: "phone", value: "123-456-7890" },
    { type: "address", value: "123 Main St" }
  ],
  skills: ["JavaScript", "Node.js", "React"],
  projects: [
    { name: "Project A", status: "completed" },
    { name: "Project B", status: "in-progress" },
    { name: "Project C", status: "planned" }
  ]
};

// Example 1: Map all items from an array
const map1 = {
  'contacts[].value': 'allContacts[]'
};

const result1 = objectMapper(source, map1);
console.log("Example 1 Result:", result1);

// Example 2: Map specific items based on a condition
const map2 = {
  'contacts[].type': {
    key: 'emailContact',
    transform: function(value: string, source: Contact): string | undefined {
      return value === 'email' ? source.value : undefined;
    }
  }
};

const result2 = objectMapper(source, map2);
console.log("Example 2 Result:", result2);

// Example 3: Map multiple array items to different keys
const map3 = {
  'skills[0]': 'primarySkill',
  'skills[1]': 'secondarySkill',
  'skills[2]': 'tertiarySkill'
};

const result3 = objectMapper(source, map3);
console.log("Example 3 Result:", result3);

// Example 4: Transform array items
const map4 = {
  'projects[].name': {
    key: 'projectNames[]',
    transform: function(value: string): string {
      return value.toUpperCase();
    }
  }
};

const result4 = objectMapper(source, map4);
console.log("Example 4 Result:", result4);

// Example 5: Filter and map array items
const map5 = {
  'projects[].status': {
    key: 'activeProjects[]',
    transform: function(value: string, source: Project): string | undefined {
      return value !== 'completed' ? source.name : undefined;
    }
  }
};

const result5 = objectMapper(source, map5);
console.log("Example 5 Result:", result5);
